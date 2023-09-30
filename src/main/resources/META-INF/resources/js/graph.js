/**
 * This is the code to generate an Alckemys graph.
 *
 * author: rlh
 * date: March 2021
 */

var jsonString = '{"nodes": [{"id": 1},{"id": 2},{"id": 3}, {"id": 4}],"edges": [{"source": 1,"target": 2},{"source": 1,"target": 3}]}';
var config;
var other_data;
var _nodeTypesVal = {
		"type":  ["rol", "facultad", "perfil", "usuario","grupo","compania"]
	};
var _nodeCaptionVal =  function(node) {
		if (node.type == "usuario")
			return node.id + ":" + node.caption;
		else
			return node.caption;
	};
var _nodeStyleVal = {
		"all": {
			"borderColor": "#127DC1",
			"color": function(d) {
				return "rgba(104, 185, 254, " +
					(d.getProperties().subTypeVal * 50) + " )"
			},
			"radius": function(d) {
				if(d.getProperties().subType)
					return 10; else return 5
			}
		},
		"rol" :{
			"color": "#B8206E",
			"selected": { "color": "#ffffff" },
			"highlighted": { "color":" #E7298A" },
			"radius": 13
		},
		"perfil" :{
			"color": "#D95F02",
			"selected": { "color": "#ffffff" },
			"highlighted": { "color":" #E7298A" },
			"radius": 16
		},
		"usuario": {
			"borderColor": "#1B9E77",
			"color": function (d) {
				return "rgba(104, 185, 254, " +
					(d.getProperties().subTypeVal * 30) + " )"
			},
			"radius": function (d) {
				if (d.getProperties().subType)
					return 10; else return 5
			}
		},
		"grupo": {
			"borderColor": "#66A61E",
			"color": function(d) {
				return "rgba(104, 185, 254, " +
					(d.getProperties().subTypeVal * 50) + " )"
			},
			"radius": function(d) {
				if(d.getProperties().subType)
					return 15; else return 13
			}
		},
		"compania": {
			"borderColor": "##E6AB02",
			"color": function(d) {
				return "rgba(104, 185, 254, " +
					(d.getProperties().subTypeVal * 50) + " )"
			},
			"radius": function(d) {
				if(d.getProperties().subType)
					return 15; else return 13
			}
		}
	};
var _edgeStyleVar = {
		"all": {
			"width": function(d) {
				return (d.getProperties().type + 0.5) * 1.3
			},
			"color" : "#000000"
		}
	};
var graphHeight = 250;
var graphWidth = 350;
var alchemy;
var circle;

function renderGraph() {
	d3.select('#graph').append('div')
		.attr('class', "alchemy")
		.attr('id', "alchemy");

	// other_data = JSON.parse(jsonString);
	config = {
//		      dataSource: 'http://localhost:8090/frontend/js/alchemy/data/contrib.json',
		dataSource: other_data,
		forceLocked: false,
		directedEdges: true,
		graphHeight: function(){ return graphHeight; },
		graphWidth: function(){ return graphWidth; },
		linkDistance: function(){ return 40; },
		backgroundColor: "#ffffff",
		nodeTypes: _nodeTypesVal,
		nodeCaption: _nodeCaptionVal,
		nodeCaptionsOnByDefault: true,
		nodeStyle: _nodeStyleVal,
		edgeCaption: function(edge) { return edge.caption;},
		edgeStyle: _edgeStyleVar
	};

	alchemy = new Alchemy(config);
}

function initOnClick() {
	d3.selectAll('circle')
		.on('click', function(d, i) {
			graph.$server.selectedNode(d.id, i);
		});
}

function updateGraph() {
	config = {
		dataSource: other_data,
		forceLocked: false,
		directedEdges: true,
		graphHeight: function(){ return graphHeight; },
		graphWidth: function(){ return graphWidth; },
		linkDistance: function(){ return 40; },
		backgroundColor: "#ffffff",
		nodeTypes: _nodeTypesVal,
		nodeCaption: _nodeCaptionVal,
		nodeCaptionsOnByDefault: true,
		nodeStyle: _nodeStyleVal,
		edgeCaption: function(edge) { return edge.caption;},
		edgeStyle: _edgeStyleVar
	};

	alchemy = new Alchemy(config);
}

