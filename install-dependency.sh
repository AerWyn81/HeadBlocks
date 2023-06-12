#!/bin/bash

if [[ -d ./libs ]]; then
  echo "remove existing libs directory"
  rm -r ./libs
fi

mkdir ./libs && cd ./libs || exit

download() {
	html=`curl -c ./cookie -s -L "https://drive.google.com/uc?export=download&id=$1"`
	curl -Lb ./cookie "https://drive.google.com/uc?export=download&`echo ${html}|grep -Po '(confirm=[a-zA-Z0-9\-_]+)'`&id=$1" -o $2
}

download "$1" "CMI-9.6.0.2.jar"
download "$2" "CMILib1.3.0.0.jar"
download "$3" "hologram-lib-2.1.1-SNAPSHOT.jar"