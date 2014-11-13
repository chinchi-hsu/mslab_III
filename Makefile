.PHONY: doc

all:
	javac -cp lib/\* -d bin/ src/*.java

doc:
	javadoc -cp lib/\* -d doc/ -noqualifier java.lang:java.util src/*.java

clean:
	rm -f bin/*.class
