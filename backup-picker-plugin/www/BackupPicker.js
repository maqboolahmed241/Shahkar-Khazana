var exec = require('cordova/exec');
exports.saveJson = function(filename, json, success, error) {
  exec(success || function(){}, error || function(){}, 'BackupPicker', 'saveJson', [filename, json]);
};