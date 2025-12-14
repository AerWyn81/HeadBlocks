#!/usr/bin/env python3
"""
Markdown to BBCode Converter for SpigotMC

Author: AerWyn81
License: MIT License

Copyright (c) 2025 AerWyn81

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
"""

import re
import sys


def markdown_to_bbcode(markdown_text):
    bbcode = markdown_text

    bbcode = re.sub(r'^# (.+)$', r'[SIZE=7][B]\1[/B][/SIZE]', bbcode, flags=re.MULTILINE)
    bbcode = re.sub(r'^## (.+)$', r'[SIZE=6][B]\1[/B][/SIZE]', bbcode, flags=re.MULTILINE)
    bbcode = re.sub(r'^### (.+)$', r'[SIZE=5][B]\1[/B][/SIZE]', bbcode, flags=re.MULTILINE)

    bbcode = re.sub(r'`(.+?)`', r'[FONT=Courier New]\1[/FONT]', bbcode)

    bbcode = re.sub(r'\[([^\]]*?)\n\s+([^\]]*?)\]\(', r'[\1 \2](', bbcode)

    bbcode = re.sub(r'\[([^\]]+)\]\(([^\)]+)\)', r'[URL=\2]\1[/URL]', bbcode)

    bbcode = re.sub(r'\*\*(.+?)\*\*', r'[B]\1[/B]', bbcode)
    bbcode = re.sub(r'__(.+?)__', r'[B]\1[/B]', bbcode)

    bbcode = re.sub(r'\*(.+?)\*', r'[I]\1[/I]', bbcode)
    bbcode = re.sub(r'_(.+?)_', r'[I]\1[/I]', bbcode)

    lines = bbcode.split('\n')
    result_lines = []
    in_list = False
    current_item = None

    for line in lines:
        list_match = re.match(r'^- (.+)$', line)

        is_continuation = re.match(r'^\s{2,}(.+)$', line)

        if list_match:
            if current_item is not None:
                result_lines.append(f'[*]{current_item}')

            if not in_list:
                result_lines.append('[LIST]')
                in_list = True

            current_item = list_match.group(1)
        elif is_continuation and in_list:
            if current_item is not None:
                current_item += ' ' + is_continuation.group(1).strip()
        else:
            if current_item is not None:
                result_lines.append(f'[*]{current_item}')
                current_item = None

            if in_list and line.strip() == '':
                result_lines.append('[/LIST]')
                in_list = False

            result_lines.append(line)

    if current_item is not None:
        result_lines.append(f'[*]{current_item}')
    if in_list:
        result_lines.append('[/LIST]')

    bbcode = '\n'.join(result_lines)

    bbcode = re.sub(r'^---$', '', bbcode, flags=re.MULTILINE)

    emoji_replacements = {
        '🔧': '⚙️',
        '🐛': '☎️',
        '🚀': '⛳️',
    }
    for old_emoji, new_emoji in emoji_replacements.items():
        bbcode = bbcode.replace(old_emoji, new_emoji)

    bbcode = re.sub(r'\n\n(\[LIST\])', r'\n\1', bbcode)

    bbcode = re.sub(r'(\[/SIZE\])\n\n(\[LIST\])', r'\1\n\2', bbcode)
    bbcode = re.sub(r'(\[SIZE=5\]\[B\].*?\[/B\]\[/SIZE\])\n\n', r'\1\n', bbcode)

    bbcode = bbcode.rstrip('\n')

    return bbcode


def main():
    input_file = 'changelog.md'
    output_file = 'changelog.bbcode'

    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            markdown_content = f.read()

        bbcode_content = markdown_to_bbcode(markdown_content)

        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(bbcode_content)

        print(f'Successful conversion: {input_file} -> {output_file}')
        print(f'\nYou can now copy the content of {output_file} to SpigotMC')

    except FileNotFoundError:
        print(f'Error: File {input_file} not found', file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f'Error during conversion: {e}', file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
