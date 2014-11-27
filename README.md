Dependencies
===================
- Java SE Development Kit 7 (version: 7u71)
- libFM: Factorization Machine Library (version: 1.42)


To compile the code
===================
Just type:

    make


Read data from a CSV file
===============

    java -cp bin:lib/\* Evaluator LIBFMPATH INPUTFILE OUTPUTFILE NFACTORS USERCOL ITEMCOL -csv

- **LIBFMPATH** the location of the libFM executable
- **INPUTTABLE** the input CSV file
- **OUTPUTFILE** where to store the prediction result
- **NFACTORS** the more data, the higher is this value (set to 5 if in doubt)
- **USERCOL** the column of user ID in the input CSV file (start from 0)
- **ITEMCOL** the column of item ID in the input CSV file (start from 0)


Example
-------

    java -cp bin:lib/\* Evaluator /root/libfm/libfm-1.42-src/bin/libFM input/product.csv output/product.csv 5 2 1 -csv

- libFM is located at /root/libfm/libfm-1.42-src/bin/libFM
- Reads input from "input/product.csv"
- The leave-one-out prediction results will be in the file "output/product.csv"
- Use 5 latent factors
- User IDs are in the column 2 of "input/product.csv"
- Item IDs are in the column 1 of "input/product.csv"


Read data from DB
===============

    java -cp bin:lib/\* Evaluator LIBFMPATH RATINGTABLE OUTPUTFILE NFACTORS USERFIELD ITEMFIELD -db CATEGORYTABLE ITEMTABLE CATEGORYFIELD

- **LIBFMPATH** the location of the libFM executable
- **RATINGTABLE** the data table in the database containing rating data
- **OUTPUTFILE** where to store the prediction result
- **NFACTORS** the more data, the higher is this value (set to 5 if in doubt)
- **USERFIELD** the column name of user ID in the data table
- **ITEMFIELD** the column name of item ID in the data table
- **CATEGORYTABLE** the data table in the database containing category information (optional)
- **ITEMTABLE** the data table in the database containing item data (optional)
- **CATEGORYFIELD** the column name of category (optional)


Example
-------

    java -cp bin:lib/\* Evaluator /root/libfm/libfm-1.42-src/bin/libFM product_order output/product_order.csv 5 aid pid -db

- libFM is located at /root/libfm/libfm-1.42-src/bin/libFM
- Reads input from the data table "product_order" in the database
- The leave-one-out prediction results will be in the file "output/product_order.csv"
- Use 5 latent factors
- User IDs are in the field "aid" of the table "product_order"
- Item IDs are in the field "pid" of the table "product_order"


    java -cp bin:lib/\* Evaluator /root/libfm/libfm-1.42-src/bin/libFM coupon_collection output/coupon_collection.csv 5 aid cid -db master_category coupon mcid

- libFM is located at /root/libfm/libfm-1.42-src/bin/libFM
- Reads input from the data table "coupon_collection" in the database
- The leave-one-out prediction results will be in the file "output/coupon_collection.csv"
- Use 5 latent factors
- User IDs are in the field "aid" of the table "coupon_collection"
- Item IDs are in the field "cid" of the table "coupon_collection"
- The category information is in the data table "master_category"
- The item table is "coupon"
- The category of an item is in the field "mcid"


Users and Items in the Database
-------

- Product order:
    INPUTTABLE = product_order, USERFIELD = aid, ITEMFIELD = pid, ITEMTABLE = product
- Coupon collection:
    INPUTTABLE = coupon_collection, USERFIELD = aid, ITEMFIELD = cid, ITEMTABLE = coupon
- Product attention:
    INPUTTABLE = product_attention_rate, USERFIELD = aid, ITEMFIELD = pid, ITEMTABLE = product

