# TODO parse the header. Add body to the "{{replaceme}}"tag
# write it out

import argparse
import os
import re


parser = argparse.ArgumentParser()
parser.add_argument("-body", help="from orgmode convert")
parser.add_argument("-headerfooter", help="template html doc")
parser.add_argument("-out", help="path to save doc to")
bodyregex = re.compile(r".*<body>(.*)</body>")


def read_html(path):
    with open(path, "r") as temp:
        data = temp.read()
    return data.replace("\n", " ")


def read_header(path):
    # don't strip newlines, allow me to read the css
    with open(path, "r") as temp:
        data = temp.read()
    return data


def write_html(out, html):
    with open(out, "w") as final:
        final.write(html)
    print(f"wrote to {out}")
    return


if __name__ == "__main__":
    args = parser.parse_args()
    main = read_html(args.body)
    template = read_header(args.headerfooter)
    data = bodyregex.search(main)
    if data:
        replaced = template.replace("{{REPLACEME}}", data.group(1))
        write_html(args.out, replaced)
    else:
        print("Body regex failure")
