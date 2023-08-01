let wsLocation = {
    hostname: "127.0.0.1",
    port: "9090",
}

let userName = prompt("Enter Your Name.")

// small helper function for selecting element by id
let id = id => document.getElementById(id);

//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + wsLocation.hostname + ":" + wsLocation.port + "/chat/" + userName);
ws.onmessage = event => updateChat(event.data);
ws.onclose = () => alert("WebSocket connection closed");

// Add event listeners to button and input field
id("send").addEventListener("click", () => sendAndClear(id("message").value));
id("message").addEventListener("keypress", function (e) {
    if (e.keyCode === 13) { // Send message if enter is pressed in input field
        sendAndClear(e.target.value);
    }
});

function sendAndClear(message) {
    if (message !== "") {
        if (ws.readyState == ws.CLOSED || ws.readyState == ws.CLOSING)
            alert("websocket is closed, please refresh page.")
        else
            ws.send(message);

        id("message").value = "";
    }
}

function updateChat(eventData) { // Update chat-panel and list of connected users
    let data = JSON.parse(eventData);
    let chatItem = document.createElement("p");
    let itemText = document.createTextNode(data.message);
    chatItem.appendChild(itemText);
    id("chat").appendChild(chatItem);
    id("userList").innerHTML = data.userList.map(user => "<li>" + user + "</li>").join("");
}
