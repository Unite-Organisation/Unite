# Unite API

**Author:** Dorian Guz  
**Year:** 2025  
**© All rights reserved**

---

## Project

**Unite** is a web application (soon mobile too) that connects residents of a building, neighborhood, or community. It works well for modern buildings but can also be used in older ones.

With Unite, residents can:
- Join groups by building, neighborhood, or area.
- Connect with neighbors and build community.
- Make joint decisions (e.g., administrative matters).
- Share announcements and updates.
- Request or lend items quickly.
- Book shared spaces like gyms, pools, or coworking areas.
- Organize community events (barbecues, runs, etc.).
- Report issues or request repairs.
- Exchange services.
- Pay rent or other fees.

---

## Features

### Admin (Developer / Manager)
- Create accounts and add residents.
- Manage user accounts.
- Assign residents to groups.
- Post announcements on a shared board.
- Create polls and voting (e.g., for repairs or decisions).

### User (Resident)
- Chat with other residents.
- Create private chats and neighbor groups.
- Book shared spaces.
- Report issues or request repair services.
- Vote in polls.
- Post personal announcements (e.g., services offered).
- Request items to borrow.
- Pay rent and other fees.
- Organize community events.

---

## Local running (macOS)

### Prerequisite
- Homebrew
- Docker

Follow the steps below to set up **Java JDK 21** and **Maven** on macOS.


### Java JDK
```bash
brew install openjdk@21
brew link --force --overwrite openjdk@21

export PATH="/usr/local/opt/openjdk@21/bin:$PATH"
source ~/.zshrc

#check version
java -version
```

### Maven
```bash
brew install maven

#check version
./mvnw -version
```

## Run Spring Boot app
```bash
./run_app.sh
```

---
# Swagger open API

Available at: 
```bash 
https://localhost:8080/v1/api/swagger-ui/index.html
```