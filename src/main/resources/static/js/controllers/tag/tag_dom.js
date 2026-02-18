const TAG_SELECTED_RING_CLASSES = ["ring-2", "ring-offset-1"]

export function syncSelectedButtons(selectedContainer, tagList) {
    const inputs = selectedContainer.querySelectorAll('input[name="tagIds"]')
    inputs.forEach(input => {
        const button = tagList.querySelector(`button[data-tag-id="${input.value}"]`)
        if (!button) {
            return
        }

        markTagSelected(button, button.dataset.tagColor)
    })
}

export function toggleTagSelection(selectedContainer, button) {
    const tagId = button.dataset.tagId
    const tagColor = button.dataset.tagColor
    let input = selectedContainer.querySelector(`input[name="tagIds"][value="${tagId}"]`)

    if (input) {
        input.remove()
        unmarkTagSelected(button)
        return false
    }

    input = createHiddenTagInput(tagId)
    selectedContainer.appendChild(input)
    markTagSelected(button, tagColor)
    return true
}

export function addTagToSelection(selectedContainer, tag) {
    const existingInput = selectedContainer.querySelector(`input[name="tagIds"][value="${tag.id}"]`)
    if (existingInput) {
        return false
    }

    const input = createHiddenTagInput(tag.id)
    selectedContainer.appendChild(input)
    return true
}

export function removeTagFromSelection(selectedContainer, tagId) {
    const input = selectedContainer.querySelector(`input[name="tagIds"][value="${tagId}"]`)
    if (input) {
        input.remove()
    }
}

export function markTagSelected(button, color) {
    button.classList.add(...TAG_SELECTED_RING_CLASSES)
    button.style.borderColor = color || ""
    button.style.boxShadow = color ? `0 0 0 2px ${color}` : ""
}

export function unmarkTagSelected(button) {
    button.classList.remove(...TAG_SELECTED_RING_CLASSES)
    button.style.boxShadow = ""
}

export function buildTagItemElement(tag) {
    const wrapper = document.createElement("div")
    wrapper.className = "relative inline-flex items-start"
    wrapper.dataset.tagItemId = String(tag.id)

    const tagBtn = document.createElement("button")
    tagBtn.type = "button"
    tagBtn.innerText = tag.name
    tagBtn.className = "tag-btn tag-chip"
    tagBtn.dataset.tagId = tag.id
    tagBtn.dataset.tagColor = tag.color
    tagBtn.dataset.action = "click->tag#toggle"
    tagBtn.style.borderColor = tag.color
    tagBtn.style.color = tag.color
    wrapper.appendChild(tagBtn)

    if (!tag.isDefault) {
        const deleteBtn = document.createElement("button")
        deleteBtn.type = "button"
        deleteBtn.className = "tag-delete-btn"
        deleteBtn.dataset.tagId = tag.id
        deleteBtn.dataset.tagName = tag.name
        deleteBtn.dataset.action = "click->tag#deleteTag"
        deleteBtn.title = `${tag.name} 태그 삭제`
        deleteBtn.innerHTML = '<span aria-hidden="true">×</span><span class="sr-only">태그 삭제</span>'
        wrapper.appendChild(deleteBtn)
    }

    return { wrapper, tagButton: tagBtn }
}

export function removeTagItem(tagList, tagId) {
    const tagItem = tagList.querySelector(`[data-tag-item-id="${tagId}"]`)
    if (tagItem) {
        tagItem.remove()
    }
}

function createHiddenTagInput(tagId) {
    const input = document.createElement("input")
    input.type = "hidden"
    input.name = "tagIds"
    input.value = tagId
    return input
}
