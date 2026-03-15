#!/bin/bash
find java -type f -name "*.kt" | while read -r file; do
    # Swap the 'java' root folder for 'kotlin' in the file path
    dest="${file/#java/kotlin}"
    
    # Ensure the destination subdirectory exists
    mkdir -p "$(dirname "$dest")"
    
    # Move the file
    mv "$file" "$dest"
done
