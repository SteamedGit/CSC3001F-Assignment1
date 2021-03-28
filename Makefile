JAVAC=/usr/bin/javac
.SUFFIXES: .java .class
SRCDIR=src
BINDIR=bin
DOCDIR=doc

$(BINDIR)/%.class:$(SRCDIR)/%.java
	$(JAVAC) -d $(BINDIR)/ -cp $(BINDIR) $<

CLASSES= ClientData.class ServerThread.class Server.class Client.class
CLASS_FILES=$(CLASSES:%.class=$(BINDIR)/%.class)
default: $(CLASS_FILES)
clean:
	rm $(BINDIR)/*.class

run:
	java -cp bin FindBasin "data/large_in.txt" "test.txt" "sFind" "pExtract"
docs:
	javadoc -d $(DOCDIR) src/*.java 
