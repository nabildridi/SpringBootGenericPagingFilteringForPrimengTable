import { Component } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { LazyLoadEvent } from 'primeng/api';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  columnsDefs = [
    { field: 'id', header: 'Id', sortable: true, searchable: true, type: "number" },
    { field: 'username', header: 'Username', sortable: true, searchable: true, type: "string" },
    //{ field: 'firstname', header: 'FirstName', sortable: true, searchable: true, type: "string" },
    { field: 'lastname', header: 'LastName', sortable: true, searchable: true, type: "string" },
    //{ field: 'email', header: 'Email', sortable: true, searchable: true, type: "string" },
    { field: 'accessdate', header: 'date', sortable: true, searchable: true, type: "date" },
    { field: 'modified', header: 'boolean', sortable: true, searchable: true, type: "boolean" },
	{ field: 'account', header: 'Decimal', sortable: true, searchable: true, type: "decimal" }


  ];

  settings = {
    url: "/paginate",
    rowsPerPage: 10,
    globalFilter: true,
    emptyMessage: "No records found",
    defaultSort: { field: 'id', order: 1 }
  };

  totalRecords: number = 0;
  data: any;

  constructor(private http: HttpClient) { }

  ngOnInit() { }

  loadFromServer(event: LazyLoadEvent) {
    this.http
      .post('http://localhost:8080' + this.settings.url, event)
      .subscribe({
        next: (json: any) => {
          if (json) {
            this.data = json['content'];
            this.totalRecords = json['totalElements'];
          }
        },
        error: (e) => {
          console.log('error');
        },
      });
  }
}
