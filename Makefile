JAVAC=/usr/bin/javac
.SUFFIXES: .java .class
SRCDIR=src
BINDIR=bin
DOCDIR=doc

$(BINDIR)/%.class:$(SRCDIR)/%.java
	$(JAVAC) -d $(BINDIR)/ -cp $(BINDIR) $<

CLASSES= ClientData.class RegisterClientThread.class ServerThread.class Server.class Client.class
CLASS_FILES=$(CLASSES:%.class=$(BINDIR)/%.class)
default: $(CLASS_FILES)
clean:
	rm $(BINDIR)/*.class
server:
	java -cp bin Server
docs:
	javadoc -d $(DOCDIR) src/*.java 
