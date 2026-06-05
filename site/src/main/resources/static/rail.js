(function(){
  function loadRail(){
    var xhr = new XMLHttpRequest();
    xhr.open('GET','/rail.html',true);
    xhr.onreadystatechange=function(){
      if(xhr.readyState===4 && xhr.status===200){
        var ph = document.getElementById('rail-placeholder');
        if(ph){
          var range = document.createRange();
          var frag = range.createContextualFragment(xhr.responseText);
          ph.parentNode.replaceChild(frag, ph);
          highlightActive();
        }
      }
    };
    xhr.send();
  }

  function highlightActive(){
    var path = location.pathname;
    var page = path.split('/').pop() || 'index';
    if(page === '' || page === 'index.html') page = 'index';
    document.querySelectorAll('.rail-item').forEach(function(el){
      var href = el.getAttribute('href');
      var dataPage = el.getAttribute('data-page');
      var isActive = false;
      if(href){
        if(path === href || path === href + '/'){
          isActive = true;
        }
        if(path === '/profile' && href === '/login'){
          isActive = true;
        }
      }
      if(dataPage && (page === dataPage || page.startsWith(dataPage))){
        isActive = true;
      }
      if(isActive){
        el.classList.add('active');
      }
    });
  }

  if(document.readyState === 'loading'){
    document.addEventListener('DOMContentLoaded', loadRail);
  } else {
    loadRail();
  }
})();
