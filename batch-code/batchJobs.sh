#!/bin/bash

seedSize=(1000 2000 3000 4000 5000 7000 10000 13000 16000 20000 25000 30000 35000);

for size in ${seedSize[@]}; do
echo "universe = vanilla" > "job$size.txt"
echo "environment= CLASSPATH=/u/vidhoon/condor/parser-v4.jar" >> "job$size.txt" 

echo "Initialdir = /u/vidhoon/condor/exp1" >> "job$size.txt"
echo "Executable = /u/vidhoon/condor/exp1/test.sh" >> "job$size.txt"

echo "+Group   = \"GRAD\"" >> "job$size.txt"
echo "+Project = \"INSTRUCTIONAL\"" >> "job$size.txt"
echo "+ProjectDescription = \"CS388 Homework 3\"" >> "job$size.txt"

echo "Log = /u/vidhoon/condor/exp1/experiment$size.log" >> "job$size.txt"


echo "Notification = complete" >> "job$size.txt"
echo "Notify_user = vidhoon@utexas.edu" >> "job$size.txt"
echo "Requirements = Memory > 1024" >> "job$size.txt"
echo "Arguments = $size" >> "job$size.txt"

echo "Output = job$size.out" >> "job$size.txt"
echo "Error  = job$size.err" >> "job$size.txt"
echo "Queue 1" >> "job$size.txt"

condor_submit "job$size.txt"
done




