// Node.JS externs: https://github.com/dcodeIO/node.js-closure-compiler-externs

var electron = {};
electron.dialog = function() {};
electron.app = {};
electron.app.quit = function() {};

electron.ipcRenderer = function() {};
electron.ipcMain = function() {};
electron.on = function() {};
electron.send = function() {};
electron.remote = function() {};
electron.require = function() {};
electron.buildFromTemplate = function() {};
electron.popup = function() {};
electron.getCurrentWindow = function() {};
electron.showErrorBox = function() {};
electron.setTitle = function() {};
electron.setRepresentedFilename = function() {};
electron.showMessageBox = function() {};
electron.getPath = function() {};
electron.showSaveDialog = function() {};
electron.showOpenDialog = function() {};

electron.BrowserWindow = function () {};

electron.BrowserWindow.on = function() {};
electron.BrowserWindow.once = function() {};
electron.BrowserWindow.show = function() {};
electron.BrowserWindow.loadURL = function() {};
electron.BrowserWindow.webContents = {};
electron.BrowserWindow.webContents.openDevTools = function() {};

electron.Menu = {};
electron.Menu.buildFromTemplate = function() {};
electron.Menu.setApplicationMenu = function() {};

electron.session = {};
electron.session.defaultSession = {};
electron.session.clearCache = function() {};

electron.dialog = {};
electron.dialog.showErrorBox = function () {};

var electronDl = {};
electronDl.download = function () {};
electronDl.download.then = function () {};

var decompress = function () {};
decompress.then = function () {};


/**
 * @constructor
 * @extends events.EventEmitter
 */
var process = function() {};

/**
 * @type {string}
 */
process.platform;

/**
 * @return {string}
 * @nosideeffects
 */
process.cwd = function() {};
/**
 * @type {string}
 */
process.platform;
/**
 * @type {string}
 */
process.resourcesPath;


var electronUpdater = {};
electronUpdater.autoUpdater = {};
electronUpdater.autoUpdater.on = function() {};
electronUpdater.autoUpdater.logger = function() {};
electronUpdater.autoUpdater.checkForUpdates = function() {};
electronUpdater.autoUpdater.downloadUpdate = function() {};
electronUpdater.autoUpdater.quitAndInstall = function() {};
electronUpdater.autoUpdater.setFeedURL = function() {};

var document = {};
document.querySelector = {};
document.querySelector.getWebContents = function() {};
document.querySelector.getWebContents.executeJavaScript = function() {};


var child_process = {};
child_process.ChildProcess = function(var_args) {};
child_process.ChildProcess.spawn = function(command, args, options) {};
child_process.exec = function(command, options, callback) {};
child_process.fork = function(modulePath, args, options) {};
child_process.ChildProcess.prototype.stdin;
child_process.ChildProcess.prototype.stdout;
child_process.ChildProcess.prototype.stderr;
child_process.ChildProcess.prototype.pid;


var http = {};
http.get = function(options, callback) {};
http.IncomingMessage = function() {};
http.IncomingMessage.prototype.method;
http.IncomingMessage.prototype.url;
http.IncomingMessage.prototype.headers;
http.IncomingMessage.prototype.trailers;
http.IncomingMessage.prototype.httpVersion;
http.IncomingMessage.prototype.httpVersionMajor;
http.IncomingMessage.prototype.httpVersionMinor;
http.IncomingMessage.prototype.connection;
http.IncomingMessage.prototype.statusCode;


var path = {};
path.normalize = function(p) {};

var fs = {};
fs.renameSync = function(oldPath, newPath) {};
fs.existsSync = function(path) {};
fs.readFileSync = function(path) {};

var sinek = {};
sinek.Consumer = function() {};
sinek.Consumer.connect = function() {};
sinek.Consumer.consume = function() {};
sinek.Consumer.then = function() {};
sinek.Consumer.catch = function() {};
sinek.Consumer.close = function() {};
