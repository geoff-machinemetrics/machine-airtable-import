# Machine Airtable Import

## Installing

```bash
npm i
```

## Running

```bash
npm start -- -u http://example.com -a abc123
```

## ClojureScript

The ClojureScript implementation is for demo-purposes only and is located in the `src` folder. It is split into a server part and the actual importer. Start the server with

    node dist/server.min.js

and then start the import script with

    node dist/importer.min.js
    
Install Leiningen (https://leiningen.org/) and shadow-cljs (http://shadow-cljs.org/) to build and develop the application using a REPL.

To compile the distribution files use

    shadow-cljs release server
    shadow-cljs release importer