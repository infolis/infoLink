Data model
==========


Dependencies for posting
------------------------

These are the classes of the Infolis data model with a list of entities
they depend on, in the sense of the correct order of posting instances
of these classes.

```
InfolisFile
    NONE
QueryService
    NONE
SearchResult
    QueryService
Entity
    InfolisFile
EntityLink
    Entity
TextualReference
    Entity
InfolisPattern
    TextualReference
Execution
    Entity
    EntityLink
    InfolisFile
    InfolisPattern
    QueryService
    SearchResult
    TextualReference
```
