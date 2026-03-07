#!/usr/bin/env python3
"""
Renders the AST DOT file as a graphical tree using graphviz and matplotlib.
Usage: python3 render_ast.py <file.dot> [output.png]
"""

import sys
import subprocess
import os

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 render_ast.py <file.dot> [output.png]")
        sys.exit(1)

    dot_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else "ast_tree.png"

    if not os.path.exists(dot_file):
        print(f"Error: {dot_file} not found")
        sys.exit(1)

    # try using graphviz dot command directly
    try:
        subprocess.run(
            ["dot", "-Tpng", dot_file, "-o", output_file],
            check=True,
            capture_output=True
        )
        print(f"AST tree saved to: {output_file}")
        # open the image
        if sys.platform == "darwin":
            subprocess.run(["open", output_file])
        elif sys.platform == "linux":
            subprocess.run(["xdg-open", output_file])
        return
    except FileNotFoundError:
        print("graphviz 'dot' command not found, trying python graphviz package...")
    except subprocess.CalledProcessError as e:
        print(f"dot command failed: {e.stderr.decode()}")

    # fallback to python graphviz package
    try:
        import graphviz
        with open(dot_file) as f:
            dot_source = f.read()
        src = graphviz.Source(dot_source)
        src.format = "png"
        out_path = src.render(filename=output_file.replace(".png", ""), cleanup=True)
        print(f"AST tree saved to: {out_path}")
        if sys.platform == "darwin":
            subprocess.run(["open", out_path])
        elif sys.platform == "linux":
            subprocess.run(["xdg-open", out_path])
    except ImportError:
        print()
        print("To render the AST graph, install graphviz:")
        print("  brew install graphviz        (macOS)")
        print("  sudo apt install graphviz    (Linux)")
        print("  pip install graphviz         (Python package)")
        print()
        print(f"The DOT file was saved to: {dot_file}")
        print("You can render it manually: dot -Tpng ast.dot -o ast_tree.png")
        sys.exit(1)

if __name__ == "__main__":
    main()
