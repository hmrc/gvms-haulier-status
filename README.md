
# gvms-haulier-status

This service provides updates to the Service Availability Dashboard on the status of the Haulier UI/API asynchronous create/update flows. 

## Running the service locally
`sbt run`

By default, the service runs on port 8990.

## GVMS Haulier Status API Endpoints

```POST /gvms-haulier-status/movements```

with header :

Authorization {InternalAuth token}

```
Request Body Example
{
  "id": "4eca56b3-698c-443b-bd47-14c6e3407d54"
}
```

```
Success Response

Correlation id created

 * **Code:** 201
 
 
Error response 
 
Correlation Id already created 

 * **Code:** 400

    An entry with correlation id 4eca56b3-698c-443b-bd47-14c6e3407d54 already exists

```

```PUT /gvms-haulier-status/movements/:correlationId```

with header :

Authorization {InternalAuth token}

```
Success Response

Correlation id updated

 * **Code:** 200
 
 
Error response 
 
Correlation Id not found 

 * **Code:** 404

    No entry with correlation id 4eca56b3-698c-443b-bd47-14c6e3407d54 found

```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").