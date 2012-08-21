Ext.require(['Ext.data.*', 'Ext.grid.*']);

Ext.define('Layers', {
    extend: 'Ext.data.Model',
    fields: [{
        name: 'id',
        type: 'int'
    }, {
        name: 'name',
        type: 'string'
    }]
});

Ext.onReady(function() {

    var store = Ext.create('Ext.data.Store', {
        autoLoad: true,
        autoSync: true,
        model: 'Layers',
        proxy: {
            type: 'rest',
            url: 'http://localhost:8082/sfs/capabilities',
            reader: {
                type: 'json',
                root: 'capabilities'
            },
            writer: {
                type: 'json'
            }
        }
    });
    
    var grid = Ext.create('Ext.grid.Panel', {
        renderTo: document.body,
        width: 200,
        height: 200,
        frame: true,
        title: 'Layers',
        store: store,
        columns: [{
            text: 'ID',
            width: 40,
            sortable: true,
            dataIndex: 'id'
        }, {
            text: 'NAME',
            width: 80,
            sortable: true,
            dataIndex: 'name'
        }]
    });
});