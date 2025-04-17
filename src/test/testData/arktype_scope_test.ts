import {scope, type} from "arktype";

// Define a scope with type aliases
const coolScope = scope({
    // Type aliases that should be available for completion within the scope
    Id: "string",
    User: { id: "Id", friends: "Id[]" },
    UsersById: {
        "[Id]": "User | undefined"
    }
})

// Using the scope to create a type
const group = coolScope.type({
    name: "string",
    members: "User[]"  // Should suggest "User" from the scope
})

// Another scope with different aliases
const anotherScope = scope({
    Email: "string.email",
    Person: {
        name: "string",
        email: "Email"  // Should suggest "Email" from this scope
    }
})