const path = require('path');

config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};
config.resolve.fallback.path = false;
config.resolve.fallback.fs = false;
config.resolve.fallback.crypto = false;

// Serve sql-wasm.wasm at root for the worker's runtime fetch
config.devServer = config.devServer || {};
const origSetup = config.devServer.setupMiddlewares;
config.devServer.setupMiddlewares = (middlewares, devServer) => {
    devServer.app.get('/sql-wasm.wasm', (req, res) => {
        res.sendFile(path.resolve(__dirname, '../../node_modules/sql.js/dist/sql-wasm.wasm'));
    });
    return origSetup ? origSetup(middlewares, devServer) : middlewares;
};
