#!/bin/bash

mkdir ./libs && cd ./libs || exit

download() {
	html=`curl -c ./cookie -s -L "https://drive.google.com/uc?export=download&id=$1"`
	curl -Lb ./cookie "https://drive.google.com/uc?export=download&`echo ${html}|grep -Po '(confirm=[a-zA-Z0-9\-_]+)'`&id=$1" -o $2
}

download "$1" "$2"