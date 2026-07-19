with open('app/src/main/java/com/example/HomeScreen.kt', 'r') as f:
    lines = f.readlines()[215:312]

count = 0
for i, line in enumerate(lines):
    for char in line:
        if char == '{': count += 1
        elif char == '}': count -= 1
    print(f"Line {216+i}: count={count}  {line.strip()}")
