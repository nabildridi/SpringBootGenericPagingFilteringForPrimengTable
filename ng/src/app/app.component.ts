import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LazyLoadEvent } from 'primeng/api';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  columnsDefs = [
    { field: 'id', header: 'Id', sortable: true, searchable: true, type: "number" },
    { field: 'username', header: 'Username', sortable: true, searchable: true, type: "string" },
    { field: 'firstname', header: 'FirstName', sortable: true, searchable: true, type: "string" },
    { field: 'lastname', header: 'LastName', sortable: true, searchable: true, type: "string" },
    { field: 'email', header: 'Email', sortable: true, searchable: true, type: "string" },
    { field: 'accessdate', header: 'date', sortable: true, searchable: true, type: "date" },
    { field: 'modifdate', header: 'search with range', sortable: true, searchable: true, type: "range" }
  ];

  settings = {
    url: "/paginate",
    rowsPerPage: 10,
    globalFilter: true,
    emptyMessage: "No records found",
    defaultSort: { field: 'id', order: 1 }
  };

  totalRecords: number;
  data: any;

  constructor(private http: HttpClient) { }

  ngOnInit() { }

  makeRange(rangeArray: Date[]): string {
    let ret: string = "" + rangeArray[0].getTime();
    if (rangeArray[1]) {
      ret = ret + "-" + rangeArray[1].getTime();
    }
    return ret;
  }

  loadFromServer(event: LazyLoadEvent) {

    this.http.post("http://localhost:8888" + this.settings.url, event).subscribe(
      json => {
        if (json) {
          this.data = json["content"];
          this.totalRecords = json["totalElements"];
        }
      },
      error => { console.log("error"); }
    );

  }
}
