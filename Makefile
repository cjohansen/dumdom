PATH := node_modules/.bin:$(PATH)

node_modules/.bin/karma:
	npm install

test:
	clojure -A:dev -A:test

.PHONY: test
