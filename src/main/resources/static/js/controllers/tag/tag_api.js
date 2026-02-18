function addCsrfHeaders(headers) {
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]')
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]')

    if (csrfTokenMeta && csrfHeaderMeta) {
        headers[csrfHeaderMeta.content] = csrfTokenMeta.content
    }

    return headers
}

export async function createTagRequest(apiEndpoint, payload) {
    const headers = addCsrfHeaders({ "Content-Type": "application/json" })
    return fetch(apiEndpoint, {
        method: "POST",
        headers,
        body: JSON.stringify(payload)
    })
}

export async function deleteTagRequest(apiEndpoint, tagId) {
    const headers = addCsrfHeaders({})
    return fetch(`${apiEndpoint}/${tagId}`, {
        method: "DELETE",
        headers
    })
}
