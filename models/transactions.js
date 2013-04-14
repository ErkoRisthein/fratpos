var db = require('./db');
var transactions = db.collection('transactions');
var users = require('../models/users');
var products = require('../models/products');
var paytypes = require('../models/paytypes');
var async = require('async');

exports.getAll = function(callback) {
    exports.getWithFilter({}, callback);
}

exports.get = function(id,callback){
    transactions.findOne({_id: db.ObjectId(id)}, function(err, transaction){
        addSumAndUserToTransaction(transaction, callback);
    });
}

exports.getWithFilter = function(filter, callback){
    transactions.find(filter).sort({time: -1},function(err,transactions){
        if(err != null){
            callback(err);
            return;
        }
        async.map(transactions, addSumAndUserToTransaction, function(err,transactions){
            callback(err,transactions);
        });
    });
}

var addSumAndUserToTransaction = function(item, callback){
    item.sum = exports.getTransactionSum(item);
    users.get(item.user, function(err, user){
        if(user)
            item.user = users.getUserFullName(user);
        callback(null, item);
    });
}

exports.save = function(transaction, callback){
    transactions.save(transaction,function(err){
        callback(err,exports.getTransactionSum(transaction), transaction._id);
    });
}

exports.remove = function(callback){
    console.log('Removing all transactions');
    transactions.remove(callback);
}

exports.getTransactionSum = function(transaction){
    var sum = 0;
    for(var i = 0;i< transaction.products.length;i++){
        sum += Number(transaction.products[i].price) * Number(transaction.products[i].quantity);
    }
    return sum;
}

exports.invalid = function(id, callback){
    console.log('Marking transaction '+id+' invalid');
    async.series([
        function(callback){
            transactions.findOne({_id: db.ObjectId(id), invalid: false}, callback);
        },
        function(callback){
            transactions.update({_id: db.ObjectId(id)}, {$set: {invalid: true}}, callback);
        }],
        function(err, result){
            if(err){callback(err);return;}
            async.series([
                async.apply(incrementBalance, result[0]),
                async.apply(updateProductQuantities, result[0])
                ], callback);
        });
}

var incrementBalance = function(transaction, callback) {
    console.log('Updating user balance');
    paytypes.get(transaction.type, function(err, paytype){
        if(paytype.affectsBalance){
            var sum = exports.getTransactionSum(transaction);
            users.incrementBalance(transaction.user, sum, callback);
        }
        else{
            callback(err);
        }
    });
}

var updateProductQuantities = function(transaction, callback) {
    console.log('Updating product quantities');
    var incQuantity = function(product, callback){
        products.incQuantity(product.name, product.quantity, callback);
    }
    async.each(transaction.products,incQuantity, callback);
}