var exec = require('cordova/exec');
exports.pick = function(success, error) {
  exec(success || function(){}, error || function(){}, 'ContactPicker', 'pick', []);
};