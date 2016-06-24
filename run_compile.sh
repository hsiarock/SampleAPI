
if [ "$1" = "" ]
then
    echo "Usage: $0 xxxx.java"  "   *no need to prefix \"src/com/keyasic/p2p\""
    exit 1 
else
    echo "Use javac for file " $1
fi

javac -cp src src/com/keyasic/p2p/$1

