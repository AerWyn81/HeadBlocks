#!/bin/bash

download() {
	html=`curl -c ./cookie -s -L "https://drive.google.com/uc?export=download&id=$1"`
	curl -Lb ./cookie "https://drive.google.com/uc?export=download&`echo ${html}|grep -Po '(confirm=[a-zA-Z0-9\-_]+)'`&id=$1" -o $2
}

download "$1" "hologram-lib-1.4.0-BETA.jar"
download "$2" "CMI9.0.0.0API.jar"
download "$3" "CMILib1.2.4.1.jar"