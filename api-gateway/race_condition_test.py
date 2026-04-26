import asyncio
import aiohttp
import json
import time

URL = "http://localhost:8080/api/posts/1/comments"

payload = {
    "authorId": 2,
    "authorType": "BOT",
    "content": "Concurrent spam comment"
}

headers = {
    "Content-Type": "application/json"
}

async def send_request(session, req_id):
    try:
        async with session.post(URL, json=payload, headers=headers) as response:
            status = response.status
            return status
    except Exception as e:
        return str(e)

async def main():
    print("Starting 200 concurrent requests...")
    start_time = time.time()
    
    async with aiohttp.ClientSession() as session:
        tasks = []
        # Fire 200 requests simultaneously
        for i in range(200):
            tasks.append(send_request(session, i))
            
        results = await asyncio.gather(*tasks)
    
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
    asyncio.run(main())
