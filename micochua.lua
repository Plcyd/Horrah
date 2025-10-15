io.write("Enter Num 1: ")
local a = tonumber(io.read())
io.write("Enter Op (+, -, *, /): ")
local op = io.read()
io.write("Enter Num 2: ")
local b = tonumber(io.read())
local res = "Invalid input or op"
if a and b then if op == "+" then res = a + b elseif op == "-" then res = a - b elseif op == "*" then res = a * b elseif op == "/" then res = a / b end end
if type(res) == "number" then
print("Result: " .. res) else print(res) end
