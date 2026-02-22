#!/bin/bash

###############################################################################
# Migration Script: Convert Books to Deterministic UUIDs
#
# This script migrates all existing books to use deterministic UUIDs based
# on their PDF file hash. This enables smart reconnection when books are
# deleted and re-uploaded.
#
# What it does:
# 1. Queries all books from database
# 2. For each book with a random UUID:
#    - Generates deterministic UUID from file hash
#    - Renames PDF file (old-id.pdf → deterministic-id.pdf)
#    - Renames thumbnail (old-id.png → deterministic-id.png)
#    - Renames audio folder (old-id/ → deterministic-id/)
#    - Updates database to use deterministic UUID
###############################################################################

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  Book Migration to Deterministic UUIDs"
echo "=========================================="
echo ""

# Database connection
DB_NAME="bookshelf"
DB_USER="postgres"

# Get all books that need migration
echo -e "${BLUE}Analyzing books...${NC}"
BOOKS=$(psql -U $DB_USER -d $DB_NAME -t -A -F'|' -c "
SELECT
    id,
    title,
    file_hash,
    pdf_path,
    thumbnail_path
FROM books
ORDER BY date_added;
")

MIGRATED_COUNT=0
SKIPPED_COUNT=0
ERROR_COUNT=0

# Process each book
while IFS='|' read -r CURRENT_ID TITLE FILE_HASH PDF_PATH THUMBNAIL_PATH; do
    # Skip empty lines
    if [ -z "$CURRENT_ID" ]; then
        continue
    fi

    # Generate deterministic UUID from file hash (first 32 chars formatted as UUID)
    HASH_PREFIX=$(echo "$FILE_HASH" | cut -c1-32)
    DETERMINISTIC_ID=$(echo "$HASH_PREFIX" | sed -E 's/(.{8})(.{4})(.{4})(.{4})(.{12})/\1-\2-\3-\4-\5/')

    # Check if already deterministic
    if [ "$CURRENT_ID" == "$DETERMINISTIC_ID" ]; then
        echo -e "${GREEN}✓${NC} Already deterministic: $TITLE"
        ((SKIPPED_COUNT++))
        continue
    fi

    echo ""
    echo -e "${YELLOW}Migrating: $TITLE${NC}"
    echo "  Old ID: $CURRENT_ID"
    echo "  New ID: $DETERMINISTIC_ID"

    # Start migration for this book
    ERROR=false

    # 1. Rename PDF file
    if [ -f "$PDF_PATH" ]; then
        PDF_DIR=$(dirname "$PDF_PATH")
        NEW_PDF_PATH="$PDF_DIR/$DETERMINISTIC_ID.pdf"
        echo "  → Renaming PDF..."
        if mv "$PDF_PATH" "$NEW_PDF_PATH" 2>/dev/null; then
            echo -e "    ${GREEN}✓${NC} PDF renamed"
        else
            echo -e "    ${RED}✗${NC} Failed to rename PDF"
            ERROR=true
        fi
    else
        echo -e "    ${YELLOW}!${NC} PDF not found: $PDF_PATH"
    fi

    # 2. Rename thumbnail file
    if [ -f "$THUMBNAIL_PATH" ]; then
        THUMB_DIR=$(dirname "$THUMBNAIL_PATH")
        # Thumbnail might be .jpg or .png, detect which one
        THUMB_EXT="${THUMBNAIL_PATH##*.}"
        NEW_THUMBNAIL_PATH="$THUMB_DIR/$DETERMINISTIC_ID.$THUMB_EXT"
        echo "  → Renaming thumbnail..."
        if mv "$THUMBNAIL_PATH" "$NEW_THUMBNAIL_PATH" 2>/dev/null; then
            echo -e "    ${GREEN}✓${NC} Thumbnail renamed"
        else
            echo -e "    ${RED}✗${NC} Failed to rename thumbnail"
            ERROR=true
        fi
    else
        # Try alternate extension
        THUMB_DIR=$(dirname "$THUMBNAIL_PATH")
        OLD_THUMB_JPG="$THUMB_DIR/$CURRENT_ID.jpg"
        OLD_THUMB_PNG="$THUMB_DIR/$CURRENT_ID.png"

        if [ -f "$OLD_THUMB_JPG" ]; then
            NEW_THUMBNAIL_PATH="$THUMB_DIR/$DETERMINISTIC_ID.jpg"
            echo "  → Renaming thumbnail (jpg)..."
            if mv "$OLD_THUMB_JPG" "$NEW_THUMBNAIL_PATH" 2>/dev/null; then
                echo -e "    ${GREEN}✓${NC} Thumbnail renamed"
            else
                echo -e "    ${RED}✗${NC} Failed to rename thumbnail"
                ERROR=true
            fi
        elif [ -f "$OLD_THUMB_PNG" ]; then
            NEW_THUMBNAIL_PATH="$THUMB_DIR/$DETERMINISTIC_ID.png"
            echo "  → Renaming thumbnail (png)..."
            if mv "$OLD_THUMB_PNG" "$NEW_THUMBNAIL_PATH" 2>/dev/null; then
                echo -e "    ${GREEN}✓${NC} Thumbnail renamed"
            else
                echo -e "    ${RED}✗${NC} Failed to rename thumbnail"
                ERROR=true
            fi
        else
            echo -e "    ${YELLOW}!${NC} Thumbnail not found"
        fi
    fi

    # 3. Rename audio folder (if exists)
    AUDIO_DIR="data/bookshelf/audio/$CURRENT_ID"
    if [ -d "$AUDIO_DIR" ]; then
        NEW_AUDIO_DIR="data/bookshelf/audio/$DETERMINISTIC_ID"
        echo "  → Renaming audio folder..."
        if mv "$AUDIO_DIR" "$NEW_AUDIO_DIR" 2>/dev/null; then
            AUDIO_COUNT=$(ls -1 "$NEW_AUDIO_DIR" | wc -l)
            echo -e "    ${GREEN}✓${NC} Audio folder renamed ($AUDIO_COUNT files)"
        else
            echo -e "    ${RED}✗${NC} Failed to rename audio folder"
            ERROR=true
        fi
    else
        echo "    (no audio files)"
    fi

    # 4. Update database
    if [ "$ERROR" = false ]; then
        echo "  → Updating database..."

        # Build new paths
        NEW_PDF_PATH="${PDF_PATH/$CURRENT_ID/$DETERMINISTIC_ID}"
        if [ -n "$THUMB_EXT" ]; then
            NEW_THUMBNAIL_PATH="${THUMBNAIL_PATH/$CURRENT_ID/$DETERMINISTIC_ID}"
        else
            # Determine new thumbnail extension
            if [ -f "data/bookshelf/thumbnails/$DETERMINISTIC_ID.png" ]; then
                NEW_THUMBNAIL_PATH="data/bookshelf/thumbnails/$DETERMINISTIC_ID.png"
            else
                NEW_THUMBNAIL_PATH="data/bookshelf/thumbnails/$DETERMINISTIC_ID.jpg"
            fi
        fi

        UPDATE_RESULT=$(psql -U $DB_USER -d $DB_NAME -t -c "
        UPDATE books
        SET
            id = '$DETERMINISTIC_ID',
            pdf_path = '$NEW_PDF_PATH',
            thumbnail_path = '$NEW_THUMBNAIL_PATH'
        WHERE id = '$CURRENT_ID';
        " 2>&1)

        if [[ "$UPDATE_RESULT" == *"UPDATE 1"* ]]; then
            echo -e "    ${GREEN}✓${NC} Database updated"
            echo -e "${GREEN}✓ Migration complete for: $TITLE${NC}"
            ((MIGRATED_COUNT++))
        else
            echo -e "    ${RED}✗${NC} Database update failed: $UPDATE_RESULT"
            ((ERROR_COUNT++))
        fi
    else
        echo -e "${RED}✗ Migration failed for: $TITLE (file operation errors)${NC}"
        ((ERROR_COUNT++))
    fi

done <<< "$BOOKS"

# Summary
echo ""
echo "=========================================="
echo "  Migration Summary"
echo "=========================================="
echo -e "${GREEN}Migrated:${NC} $MIGRATED_COUNT books"
echo -e "${BLUE}Skipped:${NC}  $SKIPPED_COUNT books (already deterministic)"
if [ $ERROR_COUNT -gt 0 ]; then
    echo -e "${RED}Errors:${NC}   $ERROR_COUNT books"
else
    echo -e "${GREEN}Errors:${NC}   0"
fi
echo ""

if [ $ERROR_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ All books successfully migrated to deterministic UUIDs!${NC}"
    echo ""
    echo "Future benefits:"
    echo "  • Delete and re-upload same PDFs → automatic reconnection"
    echo "  • Audio files preserved and reconnected"
    echo "  • Same book = same ID across all operations"
else
    echo -e "${RED}! Some migrations failed. Please review errors above.${NC}"
    exit 1
fi
