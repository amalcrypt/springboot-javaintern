import requests

def setup():
    payload = {
        "authorId": 1,
        "authorType": "USER",
        "content": "Initial Post"
    }
    headers = {"Content-Type": "application/json"}
    
    response = requests.post("http://localhost:8080/api/posts", json=payload, headers=headers)
    print("Post created:", response.json())

if __name__ == "__main__":
    setup()
