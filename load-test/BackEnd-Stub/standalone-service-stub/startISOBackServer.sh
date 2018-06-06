#!/bin/bash

# don't run ISOServer as root
if [ $UID -eq 0 ] ; then
	echo
	echo "For security reasons you should not run this script as root!"
	echo
	exit 1
fi	

# go to current directory
#cd `dirname $0`/..

# defining some variables
ISO_JAR="iso-jar"
CLASSPATH=$ISO_JAR/standalone-service-stub-1.0.jar
java -jar $CLASSPATH $@ 
