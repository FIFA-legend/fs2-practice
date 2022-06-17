db.createUser({
    user: "root",
    pwd: "0987654321KnKn",
    roles: [
        {
            role: "readWrite",
            db: "fs2practice"
        }
    ]
})