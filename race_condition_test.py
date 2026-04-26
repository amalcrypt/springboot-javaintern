import concurrent.futures
import requests
import time

HEADERS = {"Content-Type": "application/json"}

def send_request(post_id, req_id):
    url = f"http://localhost:8080/api/posts/{post_id}/comments"
    payload = {
        "authorId": 1000 + req_id,
        "authorType": "BOT",
        "content": f"Concurrent spam comment from bot {1000+req_id}"
    }
    try:
        response = requests.post(url, json=payload, headers=HEADERS)
        return response.status_code
    except Exception as e:
        return str(e)

def main():
    # 1. Create a fresh post
    post_payload = {"authorId": 1, "authorType": "USER", "content": "Test Post for Race Condition"}
    resp = requests.post("http://localhost:8080/api/posts", json=post_payload, headers=HEADERS)
    post_id = resp.json()["id"]
    print(f"Created fresh post with ID: {post_id}")

    print("Starting 200 concurrent bot requests...")
    start_time = time.time()
    
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=200) as executor:
        futures = [executor.submit(send_request, post_id, i) for i in range(200)]
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())
    
    end_time = time.time()
    
    success_count = results.count(201)
    rate_limit_count = results.count(429)
    other_errors = len(results) - success_count - rate_limit_count
    
    print(f"Total time taken: {end_time - start_time:.2f} seconds")
    print(f"Successful inserts (201 Created): {success_count}")
    print(f"Rate limited requests (429 Too Many Requests): {rate_limit_count}")
    print(f"Other errors: {other_errors}")
    
    if success_count == 100:
        print("Test PASSED: Exactly 100 comments allowed.")
    else:
        print(f"Test FAILED: {success_count} comments allowed.")

if __name__ == "__main__":
    main()
