#!/bin/bash

if [[ -d ./libs ]]; then
  echo "remove existing libs directory"
  rm -r ./libs
fi

mkdir ./libs && cd ./libs || exit

download() {
	html=`curl "https://drive.usercontent.google.com/download?id=$1&confirm=y" -o "$2"`
}

download "$1" "CMI-9.7.3.2.jar"
download "$2" "CMILib1.4.7.16.jar"
download "$3" "holoeasy-core-3.4.4.jar"