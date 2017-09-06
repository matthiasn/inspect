const {SpellCheckHandler, ContextMenuListener, ContextMenuBuilder} = require('electron-spellchecker');

window.spellCheckHandler = new SpellCheckHandler();
window.spellCheckHandler.attachToInput();

window.spellCheckHandler.switchLanguage('en-US');
window.spellCheckHandler.autoUnloadDictionariesOnBlur();

window.contextMenuBuilder = new ContextMenuBuilder(window.spellCheckHandler);
window.contextMenuListener = new ContextMenuListener((info) => {
    window.contextMenuBuilder.showPopupMenu(info);
});
