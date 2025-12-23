RELEASE_VERSION="1.0.6"
DOC_DATE=$(shell date)

default: index.html qnx-buildfile-lang-documentation.pdf

index.html: main.adoc
	asciidoctor main.adoc -o index.html -a release_version=$(RELEASE_VERSION) -a doc_date="$(DOC_DATE)"

qnx-buildfile-lang-documentation.pdf: main.adoc
	asciidoctor-pdf main.adoc -o qnx-buildfile-lang-documentation.pdf -a release_version=$(RELEASE_VERSION) -a doc_date="$(DOC_DATE)"

clean:
	rm index.html qnx-buildfile-lang-documentation.pdf || true
