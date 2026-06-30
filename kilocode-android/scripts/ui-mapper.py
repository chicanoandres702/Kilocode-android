import xml.etree.ElementTree as ET
import re

def get_center(bounds_str):
    # bounds format: [left,top][right,bottom]
    match = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds_str)
    if match:
        l, t, r, b = map(int, match.groups())
        return (l + r) // 2, (t + b) // 2
    return None

tree = ET.parse('/tmp/ui.xml')
root = tree.getroot()

# Example: find a button
for node in root.iter('node'):
    if 'text' in node.attrib and node.attrib['text'] == 'Ask anything':
        center = get_center(node.attrib['bounds'])
        print(f"Coordinates for '{node.attrib['text']}': {center}")
        break
