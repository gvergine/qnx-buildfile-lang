default: index.html qnx-buildfile-lang-documentation.pdf

index.html: main.adoc
	asciidoctor main.adoc -o index.html

qnx-buildfile-lang-documentation.pdf: main.adoc
	asciidoctor-pdf main.adoc -o qnx-buildfile-lang-documentation.pdf

clean:
	rm index.html qnx-buildfile-lang-documentation.pdf || true
