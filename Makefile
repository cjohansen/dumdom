PATH := node_modules/.bin:$(PATH)

node_modules/.bin/karma:
	npm install

test: node_modules/.bin/karma
	clojure -A:dev -A:test
	clojure -A:dev -A:test-clj

dumdom.jar: src/dumdom/* src/dumdom/dom/*
	rm dumdom.jar && clj -A:jar

deploy: test dumdom.jar
	mvn deploy:deploy-file -Dfile=dumdom.jar -DrepositoryId=clojars -Durl=https://clojars.org/repo -DpomFile=pom.xml

.PHONY: test deploy
