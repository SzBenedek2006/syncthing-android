#!/bin/sh

if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
	printf "--help or -h: displays help message\n--cleanup: deletes backup files"
	exit
fi

if [ "${PWD%/syncthing-android/scripts}" != "$PWD" ]; then
	cd ..
fi

if [ ! "$(basename "$PWD")" = "syncthing-android" ]; then
	echo "You are in $PWD, exiting now!"
	exit
fi

if [ "$1" = "--cleanup" ]; then
	echo "Removing backup files..."
	for file in app/src/main/res/values*/.strings.xml; do
		if [ -f "$file" ]; then
			rm -v "$file"
		fi
	done
	echo
	exit
elif [ -n "$1" ]; then
	# Unknown option
	echo "Usage: $0 [--cleanup]"
    echo "Unknown option: $1"
    exit 1
fi

echo "Making backup if needed..."
for file in app/src/main/res/values*/strings.xml; do
	if [ -f "$file" ]; then
		printf "Backing up "
		if [ ! -f "${file%/*}/.strings.xml" ]; then
			cp -v "$file" "${file%/*}/.strings.xml"
		fi
	fi
done
if [ "$1" = "--backup" ]; then
	exit
fi 


base_file="app/src/main/res/values/strings.xml"
if [ ! -f "$base_file" ]; then
	echo "$base_file not found, exiting!"
	exit 1
fi
for file in app/src/main/res/values*/strings.xml; do
	if [ -f "$file" ]; then

		# Skip editing the base reference file itself
		if [ "$file" = "$base_file" ]; then
			continue
		fi

		# Backup check
		backup_file="${file%/*}/.strings.xml"
		if [ ! -f "$backup_file" ]; then
			echo "Backup missing for file $file, exiting now!"
			exit
		fi

		printf "Editing $file:"

		# Editing a temp file, not in place
		tmp_file="${file%/*}/tmp_strings.xml"
		> "$tmp_file"

		# The awk script was written by LLM
		awk '
		NR == FNR {
			# PASS 1: Read the localized backup file and save its tags to memory
			if (in_tag == 0) {
				# Match the opening of an XML tag that has a "name=" attribute
				if ($0 ~ /^[ \t]*<[a-zA-Z0-9_-]+[ \t]+.*name="/) {
					in_tag = 1
					
					# Extract the type of tag (e.g., string, string-array, plurals)
					tag_type = $0
					sub(/^[ \t]*</, "", tag_type)
					sub(/[ \t>].*/, "", tag_type)
					
					# Safely extract the name attribute value (non-greedy)
					match($0, /name="[^"]+"/)
					name = substr($0, RSTART + 6, RLENGTH - 7)
					
					buffer = $0
					
					# Check if the tag is completely closed on this very same line
					close_regex = "</" tag_type ">|/>"
					if ($0 ~ close_regex) {
						in_tag = 0
						tags[name] = buffer
					}
				}
			} else {
				# We are inside a multi-line tag; keep appending lines
				buffer = buffer "\n" $0
				close_regex = "</" tag_type ">"
				if ($0 ~ close_regex) {
					in_tag = 0
					tags[name] = buffer
				}
			}
			next
		}
		{
			# PASS 2: Read the base file to duplicate its layout
			if (in_tag_base == 0) {
				if ($0 ~ /^[ \t]*<[a-zA-Z0-9_-]+[ \t]+.*name="/) {
					in_tag_base = 1
					
					tag_type = $0
					sub(/^[ \t]*</, "", tag_type)
					sub(/[ \t>].*/, "", tag_type)
					
					match($0, /name="[^"]+"/)
					name = substr($0, RSTART + 6, RLENGTH - 7)
					
					close_regex = "</" tag_type ">|/>"
					if ($0 ~ close_regex) {
						in_tag_base = 0
						# Only print if this tag exists in our localized backup
						if (name in tags) {
							print tags[name]
						}
					}
				} else {
					# This is a structural line (comments, <resources>, blank spaces). Copy it exactly.
					print $0
				}
			} else {
				# Skip all other lines of this tag in the base file until it closes
				close_regex = "</" tag_type ">"
				if ($0 ~ close_regex) {
					in_tag_base = 0
					if (name in tags) {
						print tags[name]
					}
				}
			}
		}
		' "$backup_file" "$base_file" > "$tmp_file"

		mv "$tmp_file" "$file"
		echo "Done with $file."
	fi
done

