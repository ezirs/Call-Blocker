with open("app/src/main/java/com/ezirs/MainActivity.kt", "r") as f:
    code = f.read()
code = code.replace("                }\n            }\n        }\n\n        // Main Content (Rules)", "                }\n            }\n            }\n        }\n\n        // Main Content (Rules)")
with open("app/src/main/java/com/ezirs/MainActivity.kt", "w") as f:
    f.write(code)
