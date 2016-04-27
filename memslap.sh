set -x

memslap -s 127.0.0.1:11211 -t 30s -T 4 -c 32 -X 15 -S 2s
memslap -s 127.0.0.1:11211 -t 30s -T 4 -c 32 -X 1000 -S 2s
