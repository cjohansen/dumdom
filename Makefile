PATH := node_modules/.bin:$(PATH)

snabbdom: .gitmodules
	git submodule update --init && cd snabbdom && npm install

snabbdom/es: snabbdom
	cd snabbdom && npm run compile

src/snabbdom/snabbdom.js: snabbdom/es snabbdom/es/**/*.js src/snabbdom/source.js
	./node_modules/.bin/browserify -o src/snabbdom/snabbdom.js -s snabbdom src/snabbdom/source.js

src/snabbdom/snabbdom.min.js: snabbdom/es/**/*.js src/snabbdom/source.js
	./node_modules/.bin/browserify -o src/snabbdom/snabbdom.min.js -t uglifyify -s snabbdom src/snabbdom/source.js && uglify -s src/snabbdom/snabbdom.min.js -o src/snabbdom/snabbdom.min.js

foreign-libs: src/snabbdom/snabbdom.js src/snabbdom/snabbdom.min.js

node_modules/.bin/karma:
	npm install

test: src/snabbdom/snabbdom.js src/snabbdom/snabbdom.min.js
	clojure -A:dev -A:test

.PHONY: test foreign-libs
