const Clipboard = require('clipboard');

const clipboard = new Clipboard('.doc-swagger-to-html .sample-snippet__copy-btn', {
  text: function (triggerElem) {
    const preBlock = triggerElem.parentNode.querySelector('pre') ;
    const textToCopy = preBlock.textContent;
    return textToCopy;
  }
});

// Show the tooltip;
clipboard.on('success', function (event){
  const trigger = event.trigger;
  trigger.classList.add('dc--has-tooltip');
  trigger.classList.add('dc--has-tooltip--bottom');

  trigger.addEventListener('mouseleave', (event) => {
    event.stopPropagation();
    trigger.classList.remove('dc--has-tooltip');
    trigger.classList.remove('dc--has-tooltip--bottom');
  });
});
