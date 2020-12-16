# extract SQLite3 data to dict example, explained in this post:
# https://nickgeorge.net/programming/python-sqlite3-extract-to-dictionary/
# run with:
# python3 extract-to-dict.py
import os
import sqlite3
import sys


def sql_data_to_list_of_dicts(path_to_db, select_query):
    """Returns data from an SQL query as a list of dicts."""
    try:
        con = sqlite3.connect(path_to_db)
        con.row_factory = sqlite3.Row
        things = con.execute(select_query).fetchall()
        unpacked = [{k: item[k] for k in item.keys()} for item in things]
        return unpacked
    except Exception as e:
        print(f"Failed to execute. Query: {select_query}\n with error:\n{e}")
        return []
    finally:
        con.close()


def make_database(path_to_db, schema, data):
    """create sample db and add data"""
    try:
        db = sqlite3.connect(path_to_db)
        db.execute(schema)
        db.commit()
        for item in data:
            db.execute(
                "INSERT INTO data (fpath, n_measure, treatment, amplitude) VALUES(:fpath, :n_measure, :treatment, :amplitude)",
                item,
            )
            db.commit()  # commit after ever addition
            print(f"Added data {item['fpath']}")
        print("Done adding sample data")
    except Exception as e:
        print(f"Try deleting the database {path_to_db} and running again")
        print(f"Exception is {e}")
    finally:
        db.commit()
        db.close()


SAMPLE_DB = "temp_db.db"

SCHEMA = """CREATE TABLE data (fpath TEXT, n_measure INTEGER, treatment TEXT,
                               amplitude REAL)"""

TEST_DATA = [
    {
        "fpath": "path/to/file/one.dat",
        "n_measure": 1,
        "treatment": "Control",
        "amplitude": 50.5,
    },
    {
        "fpath": "path/to/file/two.dat",
        "n_measure": 2,
        "treatment": "Control",
        "amplitude": 76.5,
    },
    {
        "fpath": "path/to/file/three.dat",
        "n_measure": 1,
        "treatment": "Experimental",
        "amplitude": 5.5,
    },
]
if __name__ == "__main__":
    make_database(SAMPLE_DB, SCHEMA, TEST_DATA)
    # get it back out
    returned_data = sql_data_to_list_of_dicts(SAMPLE_DB, "SELECT * FROM data")
    if returned_data == TEST_DATA:
        print("Success! returned_data == input data")
        os.remove(SAMPLE_DB)  # cleanup!
        sys.exit(f"Removing database {SAMPLE_DB} and exiting.")
    else:
        print("Oops, something went wrong!!!")
        print("Returned_data != input data")
        print(f"Returned data: \n {returned_data}")
        print(f"Input data: \n {TEST_DATA}")
        os.remove(SAMPLE_DB)
        sys.exit(f"Removing database {SAMPLE_DB} and exiting.")
